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
    private Long id;
    private String name;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String nationality;
    private String position;
    private Integer shirtNumber;
    private String lastUpdated;
    private Team currentTeam;
    private Double rating;
}
