package pickup_shuttle.pickup.domain.user.dto.response;

import lombok.Builder;
import pickup_shuttle.pickup.domain.beverage.dto.response.BeverageRp;

import java.util.List;

@Builder
public record ReadWriterBoardBeforeRp(
        Long boardId,
        String shopName,
        String destination,
        List<BeverageRp> beverages,
        int tip,
        String request,
        Long finishedAt,
        boolean isMatch
) implements ReadWriterBoard { }
