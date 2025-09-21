package unq.desapp.futbol.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Player {
    private String name;
    private Integer age;
    private String nationality;
    private String position;
    private Double rating;
    private Integer matches;
    private Integer goals;
    private Integer assist;
    private Integer redCards;
    private Integer yellowCards;
}
