package socialnet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudinary.json.JSONArray;
import org.cloudinary.json.JSONException;
import socialnet.api.response.WeatherRs;
import socialnet.model.entities.Weather;
import socialnet.model.entities.City;
import socialnet.repository.CitiesRepository;
import socialnet.repository.WeatherRepository;
import org.cloudinary.json.JSONObject;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final CitiesRepository citiesRepository;
    private final WeatherRepository weatherRepository;

    @Value("${socialNetwork.weather.city-id-path}")
    private String cityIdPath;
    @Value("${socialNetwork.weather.data-path}")
    private String dataPath;
    @Value("${socialNetwork.weather.token.header}")
    private String header;
    @Value("${socialNetwork.weather.token.value}")
    private String token;
    @Value("${socialNetwork.timezone}")
    private String timezone;

    public Integer getGismeteoCityId(City city) {
        Integer cityId = null;
        try {
            URLConnection connection = new URL(cityIdPath + city.getName()).openConnection();
            connection.addRequestProperty(header, token);
            String jsonData = new String(connection.getInputStream().readAllBytes());
            log.info("Got JSON from GisMeteo API: " + jsonData);
            JSONArray citiesItems = new JSONObject(jsonData).getJSONObject("response").getJSONArray("items");
            for (int i = 0; i < citiesItems.length(); i++) {
                JSONObject currentCity = citiesItems.getJSONObject(i);
                if (!currentCity.getJSONObject("country").getString("code")
                        .equals(city.getCountry().getCodeTwoSymbols()) ||
                        !currentCity.getString("name").equals(city.getName())) {
                    continue;
                }
                if (cityId != null && !currentCity.getJSONObject("district").getString("name")
                        .equals(city.getDistrict())) {
                    continue;
                }
                cityId = currentCity.getInt("id");
            }
        } catch (IOException | JSONException e) {
            log.error("Error getting GismeteoId: " + e.getMessage());
        }
        return cityId;
    }

    @Scheduled(cron = "${socialNetwork.scheduling.weather}", zone = "${socialNetwork.timezone}")
    public void getWeather() {
        citiesRepository.findGismeteoIds().forEach(id -> {
            try {
            URLConnection connection = new URL(dataPath + id + "/").openConnection();
            connection.addRequestProperty(header, token);
            String jsonData = new String(connection.getInputStream().readAllBytes());
            JSONObject weatherInfo = new JSONObject(jsonData).getJSONObject("response");
            Weather weather = new Weather();
            weather.setGismeteoId(id);
            weather.setTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(
                    weatherInfo.getJSONObject("date").getLong("unix")),
                    ZoneId.of(timezone)));
            weather.setWeatherDescription(weatherInfo.getJSONObject("description").getString("full"));
            weather.setTemperature(weatherInfo.getJSONObject("temperature").getJSONObject("air").getDouble("C"));
            weatherRepository.save(weather);
            } catch (IOException ignored) {}
        });
    }

    @Named("getWeatherResponse")
    public WeatherRs getWeatherResponse(String cityName) {
        WeatherRs weatherRs = new WeatherRs();
        if (cityName != null) {
            List<String> cityFields = GeolocationsService.getCityFields(cityName);
            City city = citiesRepository.findCityByNameAndDistrictAndSubDistrict(
                    cityFields.get(0), cityFields.get(1), cityFields.get(2)).orElse(new City());
            if (city.getGismeteoId() != null) {
                weatherRepository.findFirstByGismeteoIdOrderByTimeDesc(city.getGismeteoId()).ifPresent(weather -> {
                    weatherRs.setCity(cityName);
                    weatherRs.setTemp(String.valueOf(weather.getTemperature()));
                    weatherRs.setClouds(weather.getWeatherDescription());
                    weatherRs.setDate(String.valueOf(weather.getTime()));
                });
            }
        }
        return weatherRs;
    }
}
