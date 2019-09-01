package de.wasenweg.alfred.scanner;

import com.fasterxml.jackson.databind.JsonNode;

import de.wasenweg.alfred.comics.Comic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class ApiMetaDataReader {

  private ComicVineService comicVineService;

  private Logger logger = LoggerFactory.getLogger(ApiMetaDataReader.class);

  private List<ScannerIssue> scannerIssues = new ArrayList<ScannerIssue>();
  private Pattern pattern;

  @Autowired
  public ApiMetaDataReader(final ComicVineService comicVineService) {
    this.comicVineService = comicVineService;

    final String publisherDirPattern = "^.*?(?<publisher>[^/]+)/";
    final String seriesDirPattern = "(?<series1>[^/]+) \\((?<volume1>\\d{4})\\)/";
    final String fileNamePattern = "(?<series2>[^/]+) (?<number>[\\d\\.a½/]+) \\((?<volume2>\\d{4})\\)( [^/]+)?\\.cbz$";
    this.pattern = Pattern
        .compile(publisherDirPattern + seriesDirPattern + fileNamePattern);
  }

  private List<String> findMissingAttributes(final Comic comic) {
    final List<String> missingAttributes = new ArrayList<String>();
    if (comic.getPublisher() == null) {
      missingAttributes.add("publisher");
    }
    if (comic.getSeries() == null) {
      missingAttributes.add("series");
    }
    if (comic.getVolume() == null) {
      missingAttributes.add("volume");
    }
    if (comic.getNumber() == null) {
      missingAttributes.add("number");
    }
    return missingAttributes;
  }

  private Boolean isValid(final Comic comic) {
    return this.findMissingAttributes(comic).isEmpty();
  }

  private String mapPosition(final String number) {
    try {
      return MetaDataReaderUtil.mapPosition(number);
    } catch (final InvalidIssueNumberException exception) {
      this.logger.warn(exception.getMessage(), exception);
      this.scannerIssues.add(ScannerIssue.builder()
          .message(exception.getMessage())
          .type(ScannerIssue.Type.WARNING)
          .build());
      return new DecimalFormat("0000.0").format(new BigDecimal(0));
    }
  }

  /**
   * Expected format:
   * `/{publisher}/{series}/{series} #{number} ({volume}).cbz`
   */
  private void setPathParts(final Comic comic) {
    final Matcher matcher = this.pattern.matcher(comic.getPath());
    if (matcher.matches()
        && matcher.group("series1").equals(matcher.group("series2"))
        && matcher.group("volume1").equals(matcher.group("volume2"))) {
      comic.setPublisher(matcher.group("publisher"));
      comic.setSeries(matcher.group("series1"));
      comic.setVolume(matcher.group("volume1"));
      comic.setNumber(matcher.group("number"));
      comic.setPosition(this.mapPosition(comic.getNumber()));
    }
  }

  /**
   * Extract meta data from file path and match against API.
   *
   * @param comic The comic book entity.
   * @return
   */
  public List<ScannerIssue> set(final Comic comic) throws Exception {
    this.scannerIssues.clear();

    if (!this.isValid(comic)) {
      // Attempt to extract meta data from file path
      this.setPathParts(comic);
    }

    // If neither the XML nor the file path contain enough hints about which
    // comic book this is, we inform the user.
    final List<String> missingAttributes = this.findMissingAttributes(comic);
    if (missingAttributes.size() > 0) {
      throw new IncompleteMetaDataException(missingAttributes);
    }

    // Here we can assume to have enough meta data about the comic to make
    // a query to the Comic Vine API.
    try {
      this.query(comic);
    } catch (final Exception exception) {
      throw new ComicVineApiException();
    }

    return this.scannerIssues;
  }

  private List<JsonNode> filterVolumeSearchResults(
      final String publisher, final String series, final String volume, final JsonNode results) {
    final Stream<JsonNode> volumes = IntStream.range(0, results.size()).mapToObj(results::get);
    return volumes.filter(v -> {
      return publisher.equals(v.get("publisher").get("name").asText())
          && series.equals(v.get("name").asText())
          && volume.equals(v.get("start_year").asText());
    }).collect(Collectors.toList());
  }

  private String findIssueDetailsUrl(final Comic comic, final List<JsonNode> issues) throws Exception {
    final List<JsonNode> filteredIssues = issues.stream()
        .filter(issue -> {
          return issue.get("issue_number").asText().equals(comic.getNumber());
        })
        .collect(Collectors.toList());

    if (filteredIssues.size() == 0) {
      throw new NoMatchException();
    }
    if (filteredIssues.size() > 1) {
      throw new NoUniqueMatchException();
    }

    return filteredIssues.get(0).get("api_detail_url").asText();
  }

  @Cacheable("volumeIds")
  private String findVolumeId(final String publisher, final String series, final String volume) throws Exception {
    int page = 0;
    JsonNode response = this.comicVineService.findVolumesBySeries(series, page);
    List<JsonNode> results = this.filterVolumeSearchResults(publisher, series, volume, response.get("results"));

    final int totalCount = response.get("number_of_total_results").asInt();
    final int limit = response.get("limit").asInt();
    final int lastPage = totalCount / limit;
    while (results.size() == 0 && page < lastPage) {
      page++;
      response = this.comicVineService.findVolumesBySeries(series, page);
      results = this.filterVolumeSearchResults(publisher, series, volume, response.get("results"));
    }

    if (results.size() > 0) {
      return results.get(0).get("id").asText();
    } else {
      throw new NoVolumeResultException();
    }
  }

  @Cacheable("volumeIssues")
  private List<JsonNode> findVolumeIssues(final String volumeId) throws Exception {
    int page = 0;
    final JsonNode response = this.comicVineService.findIssuesInVolume(volumeId, page);
    JsonNode results = response.get("results");
    final List<JsonNode> issues = IntStream.range(0, results.size()).mapToObj(results::get)
        .collect(Collectors.toList());

    final int totalCount = response.get("number_of_total_results").asInt();
    final int limit = response.get("limit").asInt();
    final int lastPage = totalCount / limit;
    while (page < lastPage) {
      page++;
      results = this.comicVineService.findIssuesInVolume(volumeId, page).get("results");
      issues.addAll(IntStream.range(0, results.size()).mapToObj(results::get)
          .collect(Collectors.toList()));
    }

    if (issues.isEmpty()) {
      throw new EmptyVolumeException();
    } else {
      return issues;
    }
  }

  private String getEntities(final JsonNode entities) {
    return IntStream.range(0, entities.size()).mapToObj(entities::get)
        .map(character -> character.get("name").asText())
        .collect(Collectors.joining(", "));
  }

  private String getCharacters(final JsonNode details) {
    return this.getEntities(details.get("character_credits"));
  }

  private String getTeams(final JsonNode details) {
    return this.getEntities(details.get("team_credits"));
  }

  private String getLocations(final JsonNode details) {
    return this.getEntities(details.get("location_credits"));
  }

  /**
   * Gathers a comma separated list of persons per role.
   * @param details The array of persons
   * @return
   */
  private Map<String, String> getPersons(final JsonNode details) {
    final JsonNode persons = details.get("person_credits");
    return IntStream.range(0, persons.size())
        .mapToObj(persons::get)
        .collect(Collectors.groupingBy(
            person -> person.get("role").asText(),
            Collectors.mapping(
                person -> person.get("name").asText(),
                Collectors.joining(", "))));
  }

  private void applyIssueDetails(final String url, final Comic comic) {
    final JsonNode response = this.comicVineService.getIssueDetails(url).get("results");
    comic.setTitle(response.get("name").asText());
    comic.setSummary(response.get("description").asText());
    final String[] coverDate = response.get("cover_date").asText().split("-");
    comic.setYear(new Short(coverDate[0]));
    comic.setMonth(new Short(coverDate[1]));
    comic.setCharacters(this.getCharacters(response));
    comic.setTeams(this.getTeams(response));
    comic.setLocations(this.getLocations(response));
    final Map<String, String> persons = this.getPersons(response);
    comic.setWriter(persons.get("writer"));
    comic.setPenciller(persons.get("penciller"));
    comic.setInker(persons.get("inker"));
    comic.setColorist(persons.get("colorist"));
    comic.setLetterer(persons.get("letterer"));
    comic.setCoverArtist(persons.get("artist, cover"));
    comic.setEditor(persons.get("editor"));
    comic.setWeb(response.get("site_detail_url").asText());
  }

  private void query(final Comic comic) throws Exception {
    final String volumeId = this.findVolumeId(comic.getPublisher(), comic.getSeries(), comic.getVolume());
    final List<JsonNode> issues = this.findVolumeIssues(volumeId);
    final String issueDetailsUrl = this.findIssueDetailsUrl(comic, issues);
    this.applyIssueDetails(issueDetailsUrl, comic);
  }
}
