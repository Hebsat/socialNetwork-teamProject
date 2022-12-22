package soialNetworkApp.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@JsonInclude(NON_NULL)
@ApiModel(description = "default response from server")
public class CommonResponse<T> {

    @ApiModelProperty(value = "operation time in timestamp", required = true, example = "1670773804")
    private Long timestamp;

    @ApiModelProperty(value = "page number", example = "0")
    private Integer offset;

    @ApiModelProperty(value = "number of elements per page", example = "20")
    private Integer perPage;

    @ApiModelProperty(value = "number of elements per page", example = "20")
    private Integer itemPerPage;

    @ApiModelProperty(value = "total number of items found", example = "500")
    private Long total;

    @ApiModelProperty(value = "any type. The data we are looking for", example = "List of objects or just object")
    private T data;

    @Override
    public String toString() {
        return "CommonResponse{" +
                "data=" + data +
                '}';
    }
}