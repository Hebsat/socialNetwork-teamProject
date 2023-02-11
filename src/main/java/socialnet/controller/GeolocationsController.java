package socialnet.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import socialnet.api.response.CommonRs;
import socialnet.api.response.ErrorRs;
import socialnet.api.response.GeolocationRs;
import socialnet.service.GeolocationsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geolocations")
@RequiredArgsConstructor
@Tag(name = "geolocations-controller", description = "Create or get records about cities and countries")
public class GeolocationsController {

    private final GeolocationsService geolocationsService;

    @GetMapping("/countries")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiOperation(value = "Get all countries")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CommonRs<List<GeolocationRs>> getCountries() {

        return geolocationsService.getAllCountries();
    }

    @GetMapping("/cities/uses")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiOperation(value = "Get cities, allowed in registered user's accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CommonRs<List<GeolocationRs>> getCitiesFromPersons(@RequestParam String country) throws Exception {

        return geolocationsService.getCitiesByCountryFromUsers(country);
    }

    @GetMapping("/cities/db")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiOperation(value = "Get all cities containing in DB and starts with symbols")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CommonRs<List<GeolocationRs>> getCitiesFromDbStartsWith(@RequestParam String starts, @RequestParam String country) throws Exception {

        return geolocationsService.getAllCitiesByCountryStartsWithFromDb(country, starts);
    }

    @GetMapping("/cities/api")
    @ApiImplicitParam(name = "authorization", value = "Access Token", required = true, paramType = "header", dataTypeClass = String.class, example = "JWT token")
    @ApiOperation(value = "Get all cities from API and starts with symbols")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "\"Name of error\"",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorRs.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CommonRs<List<GeolocationRs>> getCitiesFromApiStartsWith(@RequestParam String starts, @RequestParam String country) throws Exception {

        return geolocationsService.getAllCitiesByCountryStartsWithFromApi(country, starts);
    }
}
