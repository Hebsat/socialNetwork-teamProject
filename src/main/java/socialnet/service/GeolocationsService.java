package socialnet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudinary.json.JSONArray;
import org.cloudinary.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import socialnet.api.response.GeolocationRs;
import socialnet.api.response.CommonRs;
import socialnet.errors.NotFoundException;
import socialnet.model.entities.City;
import socialnet.model.entities.Country;
import socialnet.repository.CitiesRepository;
import socialnet.repository.CountriesRepository;
import socialnet.repository.PersonsRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeolocationsService {

    private final CitiesRepository citiesRepository;
    private final CountriesRepository countriesRepository;
    private final PersonsRepository personsRepository;
    private final WeatherService weatherService;

    @Value("${socialNetwork.geolocation.countries-path}")
    private String countriesPath;
    @Value("${socialNetwork.geolocation.cities-path.main}")
    private String citiesPath1;
    @Value("${socialNetwork.geolocation.cities-path.params}")
    private String citiesPath2;
    @Value("${socialNetwork.geolocation.token}")
    private String token;
    @Value("${socialNetwork.geolocation.first-countries-in-response}")
    private List<String> firstCountriesInResponse;
    @Value("#{'${socialNetwork.geolocation.first-cities-in-response}'.split('; ')}")
    private List<String> firstCitiesInResponse;

    public void setCityGismeteoId(String cityName) throws Exception {
        List<String> cityFields = getCityFields(cityName);
        City city = citiesRepository.findCityByNameAndDistrictAndSubDistrict(cityFields.get(0), cityFields.get(1), cityFields.get(2))
                .orElseThrow(new NotFoundException("City with " + cityName + " name was not found"));
        if (city.getGismeteoId() == null) {
            city.setGismeteoId(weatherService.getGismeteoCityId(city));
            if (city.getGismeteoId() != null) {
                citiesRepository.save(city);
            }
        }
    }

    public CommonRs<List<GeolocationRs>> getAllCountries() {
        List<Country> countries = countriesRepository.findAll();
        if (countries.isEmpty()) {
            countries.addAll(getCountriesFromApi());
        }
        return getResponse(countries.stream().map(Country::getName).collect(Collectors.toList()), firstCountriesInResponse);
    }

    private List<Country> getCountriesFromApi() {
        List<Country> response = new ArrayList<>();
        try (InputStream stream = new URL(countriesPath + token).openStream()) {
            String jsonData = new String(stream.readAllBytes());
            Set<String> jsonSet = new JSONObject(jsonData).keySet();
            jsonSet.forEach(key -> {
                if (key.matches("\\D+")){
                    return;
                }
                JSONObject countryData = new JSONObject(jsonData).getJSONObject(key);
                response.add(countriesRepository.save(
                        Country.builder()
                                .name(countryData.getString("name"))
                                .fullName(countryData.getString("fullname"))
                                .internationalName(countryData.getString("english"))
                                .codeTwoSymbols(countryData.getString("id"))
                                .build()));
            });
        } catch (IOException ignored) {}
        return response;
    }

    public CommonRs<List<GeolocationRs>> getCitiesByCountryFromUsers(String countryName) throws Exception {
        List<String> citiesNames = personsRepository.getCitiesByCountry(countryName)
                .stream().filter(Objects::nonNull).collect(Collectors.toList());
        Country country = countriesRepository.findCountryByName(countryName)
                .orElseThrow(new NotFoundException("Country with " + countryName + " name was not found"));
        List<City> cities = new ArrayList<>();
        country.getCities().forEach(city -> {
            if (citiesNames.stream().anyMatch(name -> name.equals(getCityFullName(city)))) {
                cities.add(city);
            }
        });
        return getResponse(cities.stream().map(this::getCityFullName).collect(Collectors.toList()), firstCitiesInResponse);
    }

    public CommonRs<List<GeolocationRs>> getAllCitiesByCountryStartsWithFromDb(String countryName, String startsWith) throws Exception {
        startsWith = startsWith.isEmpty() ?
                startsWith : startsWith.substring(0, 1).toUpperCase() + startsWith.substring(1);
        Country country = countriesRepository.findCountryByName(countryName)
                .orElseThrow(new NotFoundException("Country with " + countryName + " name was not found"));
        List<City> cities = citiesRepository.findCitiesByCountryAndNameStartsWith(country, startsWith);
        return getResponse(cities.stream().map(this::getCityFullName).collect(Collectors.toList()), firstCitiesInResponse);
    }

    public CommonRs<List<GeolocationRs>> getAllCitiesByCountryStartsWithFromApi(String countryName, String startsWith) throws Exception {
        Country country = countriesRepository.findCountryByName(countryName)
                .orElseThrow(new NotFoundException("Country with " + countryName + " name was not found"));
        List<GeolocationRs> response = new ArrayList<>();
        try (InputStream stream = new URL(citiesPath1 + country.getName() + "&q=" + startsWith + citiesPath2 + token).openStream()) {
            String jsonData = new String(stream.readAllBytes());
            log.info("Got cities from API: " + jsonData);
            JSONArray jsonArray = new JSONObject(jsonData).getJSONArray("items");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonCity = jsonArray.getJSONObject(i);
                if (jsonCity.getString("country").equals(country.getCodeTwoSymbols())) {
                    List<String> cityFields = getCityFields(jsonCity.getString("full_name"));
                    City city = citiesRepository.findCityByNameAndDistrictAndSubDistrict(cityFields.get(0), cityFields.get(1), cityFields.get(2)).orElse(null);
                    if (city == null) {
                        city = citiesRepository.save(City.builder()
                                .country(country)
                                .name(cityFields.get(0))
                                .district(cityFields.get(1))
                                .subDistrict(cityFields.get(2))
                                .build());
                    }
                    response.add(GeolocationRs.builder()
                            .title(getCityFullName(city))
                            .build());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return CommonRs.<List<GeolocationRs>>builder().total((long) response.size()).data(response).build();
    }

    private List<GeolocationRs> setFirstsInResponse(List<GeolocationRs> geolocationResponses, List<String> firstsInResponse) {
        List<GeolocationRs> response = new ArrayList<>(geolocationResponses);
        for (int i = firstsInResponse.size(); i > 0; i--) {
            int locationPosition = response.indexOf(GeolocationRs.builder().title(firstsInResponse.get(i - 1)).build());
            if (locationPosition > -1) {
                GeolocationRs geolocationResponse = response.get(locationPosition);
                response.remove(geolocationResponse);
                response.add(0, geolocationResponse);
            }
        }
        return response;
    }

    public static List<String> getCityFields(String fullName) {
        List<String> fields = new ArrayList<>();
        if (fullName != null) {
            int cityNameEndingIndex = fullName.indexOf(" (");
            if (cityNameEndingIndex > -1) {
                fields.add(fullName.substring(0, cityNameEndingIndex));
                fields.addAll(List.of(fullName.substring(cityNameEndingIndex + 2, fullName.length() - 1).split(", ")));
            }
            else {
                fields.add(fullName);
            }
        }
        while (fields.size() < 3) {
            fields.add("");
        }
        return fields;
    }

    private String getCityFullName(City city) {
        return city.getName() + " (" + city.getDistrict() +
                (city.getSubDistrict().isEmpty() ? "" : ", " + city.getSubDistrict()) + ")";
    }

    private CommonRs<List<GeolocationRs>> getResponse(List<String> titles, List<String> firstsInResponse) {
        List<GeolocationRs> response = new ArrayList<>();
        titles.forEach(title -> response.add(GeolocationRs.builder().title(title).build()));
        response.sort(Comparator.comparing(GeolocationRs::getTitle));
        return CommonRs.<List<GeolocationRs>>builder()
                .total((long) response.size())
                .data(setFirstsInResponse(response, firstsInResponse))
                .build();
    }
}
