package pickup_shuttle.pickup.domain.board;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pickup_shuttle.pickup._core.errors.exception.Exception400;
import pickup_shuttle.pickup._core.errors.exception.Exception500;
import pickup_shuttle.pickup.domain.beverage.BeverageRepository;
import pickup_shuttle.pickup.domain.beverage.dto.BeverageDTO;
import pickup_shuttle.pickup.domain.board.dto.request.BoardAgreeRqDTO;
import pickup_shuttle.pickup.domain.board.dto.request.BoardWriteRqDTO;
import pickup_shuttle.pickup.domain.board.dto.response.*;
import pickup_shuttle.pickup.domain.board.repository.BoardRepository;
import pickup_shuttle.pickup.domain.board.repository.BoardRepositoryCustom;
import pickup_shuttle.pickup.domain.match.Match;
import pickup_shuttle.pickup.domain.match.MatchService;
import pickup_shuttle.pickup.domain.store.Store;
import pickup_shuttle.pickup.domain.store.StoreRepository;
import pickup_shuttle.pickup.domain.user.User;
import pickup_shuttle.pickup.domain.user.UserRepository;
import pickup_shuttle.pickup.security.service.JwtService;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepositoryCustom boardRepositoryCustom;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BeverageRepository beverageRepository;
    private final JwtService jwtService;
    private final MatchService matchService;

    public Slice<BoardListRpDTO> boardList(Long lastBoardId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "boardId"));
        Slice<Board> boardsSlice = boardRepositoryCustom.searchAllBySlice(lastBoardId, pageRequest);
        return getBoardListResponseDTOs(pageRequest, boardsSlice);
    }

    //boardSlice로 BoardListRpDTO 만드는 과정
    private Slice<BoardListRpDTO> getBoardListResponseDTOs(PageRequest pageRequest, Slice<Board> boardSlice) {
        List<BoardListRpDTO> boardBoardListRpDTO = boardSlice.getContent().stream()
                .map(b -> BoardListRpDTO.builder()
                        .boardId(b.getBoardId())
                        .shopName(b.getStore().getName())
                        .finishedAt(b.getFinishedAt().toEpochSecond(ZoneOffset.UTC))
                        .tip(b.getTip())
                        .match(b.isMatch())
                        .build())
                .toList();
        return new SliceImpl<>(boardBoardListRpDTO,pageRequest,boardSlice.hasNext());
    }

    @Transactional
    public BoardWriteRpDTO write(BoardWriteRqDTO requestDTO, long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new Exception400("유저가 존재하지 않습니다")
        );
        Store store = storeRepository.findByName(requestDTO.store()).orElseThrow(
                () -> new Exception400("가게가 존재하지 않습니다")
        );
        Board board = requestDTO.toBoard(user, store);
        try {
            boardRepository.save(board);
        } catch (Exception e) {
            throw new Exception500("unknown server error");
        }

        return new BoardWriteRpDTO(board.getBoardId());
    }
    public BoardDetailBeforeRpDTO boardDetailBefore(Long boardId) {
        Board board = boardRepository.mfindByBoardId(boardId).orElseThrow(
                () -> new Exception400("공고글을 찾을 수 없습니다")
        );
        List<BeverageDTO> beverageDTOS = board.getBeverages().stream().map(
                b -> BeverageDTO.builder()
                        .name(b.getName())
                        .build()
        ).toList();
        return BoardDetailBeforeRpDTO.builder()
                .boardId(board.getBoardId())
                .destination(board.getDestination())
                .request(board.getRequest())
                .tip(board.getTip())
                .finishedAt(board.getFinishedAt().toEpochSecond(ZoneOffset.UTC))
                .isMatch(board.isMatch())
                .shopName(board.getStore().getName())
                .beverage(beverageDTOS)
                .build();
    }
    //select 2번
    public BoardDetailAfterRpDTO boardDetailAfter(Long boardId) {
        Board board = boardRepository.m2findByBoardId(boardId).orElseThrow(
                () -> new Exception400("공고글을 찾을 수 없습니다")
        );
        User user = userRepository.findByUserId(board.getMatch().getUser().getUserId()).orElseThrow(
                () -> new Exception400("매칭 된 picker를 찾을 수 없습니다")
        );
        List<BeverageDTO> beverageDTOS = board.getBeverages().stream().map(
                b -> BeverageDTO.builder()
                        .name(b.getName())
                        .build()
        ).toList();

        return BoardDetailAfterRpDTO.builder()
                .boardId(board.getBoardId())
                .destination(board.getDestination())
                .request(board.getRequest())
                .tip(board.getTip())
                .finishedAt(board.getFinishedAt().toEpochSecond(ZoneOffset.UTC))
                .isMatch(board.isMatch())
                .shopName(board.getStore().getName())
                .pickerBank(user.getBank())
                .pickerAccount(user.getAccount())
                .pickerPhoneNumber(user.getPhoneNumber())
                .arrivalTime(board.getMatch().getMatchTime().plusMinutes(board.getMatch().getArrivalTime()).toEpochSecond(ZoneOffset.UTC))
                .isMatch(board.isMatch())
                .beverage(beverageDTOS)
                .build();
    }

    @Transactional
    public BoardAgreeRpDTO boardAgree(BoardAgreeRqDTO requestDTo, Long boardId, long userId) {
        Board board = boardRepository.mfindByBoardId(boardId).orElseThrow(
                () -> new Exception400("공고글을 찾을 수 업습니다")
        );
        User user = userRepository.findById(userId).orElseThrow(
                () -> new Exception400("유저가 존재하지 않습니다")
        );
        Match match = matchService.createMatch(requestDTo.arrivalTime(),user);
        if(match.getUser().getUserId() == board.getUser().getUserId()) {
            throw new Exception400("공고글 작성자는 매칭 수락을 할 수 없습니다");
        }
        board.updateMatch(match);
        List<BeverageDTO> beverageDTOS = board.getBeverages().stream().map(
                b -> BeverageDTO.builder()
                        .name(b.getName())
                        .build()
        ).toList();

        return BoardAgreeRpDTO.builder()
                .beverage(beverageDTOS)
                .shopName(board.getStore().getName())
                .tip(board.getTip())
                .destination(board.getDestination())
                .finishedAt(board.getFinishedAt().toEpochSecond(ZoneOffset.UTC))
                .request(board.getRequest())
                .arrivalTime(board.getMatch().getMatchTime().plusMinutes(board.getMatch().getArrivalTime()).toEpochSecond(ZoneOffset.UTC))
                .build();
    }
    @Transactional
    public void boardDelete(Long boardId, long userId){
        // 공고글 확인
        Board board = boardRepository.m3findByBoardId(boardId).orElseThrow(
                () -> new Exception400("공고글을 찾을 수 없습니다")
        );
        // 공고글 작성자 확인
        if(!(board.getUser().getUserId() == userId))
            throw new Exception400("공고글의 작성자가 아닙니다");
        // 매칭되었는지 확인
        if(board.getMatch() != null)
            throw new Exception400("이미 매칭된 공고글은 삭제 할 수 없습니다");
        // 삭제
        try {
            boardRepository.delete(board);
        } catch (Exception e){
            throw new Exception500("unknown server error");
        }
    }
    public void checkListBlank(List<String> beverages) {
        for(String b : beverages) {
            if(b == null || b.isEmpty())
                throw new Exception400("음료명에 빈 문자열 or null이 입력 되었습니다");
        }
    }
}