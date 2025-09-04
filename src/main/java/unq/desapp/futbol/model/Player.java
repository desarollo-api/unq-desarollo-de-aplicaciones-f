package unq.desapp.futbol.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
}
