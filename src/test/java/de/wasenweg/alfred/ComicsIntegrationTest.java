package de.wasenweg.alfred;

import de.wasenweg.alfred.comics.ComicRepository;
import de.wasenweg.alfred.progress.ProgressRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AlfredApplication.class })
@EnableAutoConfiguration
public class ComicsIntegrationTest {

    @Autowired
    private ComicRepository comicRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders
          .webAppContextSetup(context)
          .apply(springSecurity())
          .build();
    }

    @After
    public void tearDown() {
        comicRepository.deleteAll();
        progressRepository.deleteAll();
    }

    @Test
    public void getAllComics() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                ComicFixtures.COMIC_V1_1,
                ComicFixtures.COMIC_V1_2));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.comics.length()").value(2))
                .andExpect(jsonPath("$._embedded.comics[0].title").value(ComicFixtures.COMIC_V1_1.getTitle()))
                .andExpect(jsonPath("$._embedded.comics[1].title").value(ComicFixtures.COMIC_V1_2.getTitle()));
    }

    @Test
    public void findLastReadForVolumeWithReadIssue() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                ComicFixtures.COMIC_V1_1, // read
                ComicFixtures.COMIC_V1_2,
                ComicFixtures.COMIC_V1_3));

        progressRepository.save(ProgressFixtures.comicRead(ComicFixtures.COMIC_V1_1));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findLastReadForVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext())
                .param("publisher", ComicFixtures.COMIC_V1_1.getPublisher())
                .param("series", ComicFixtures.COMIC_V1_1.getSeries())
                .param("volume", ComicFixtures.COMIC_V1_1.getVolume()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.title").value(ComicFixtures.COMIC_V1_2.getTitle()));
    }

    @Test
    public void findLastReadForVolumeWithStartedIssue() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                ComicFixtures.COMIC_V1_1, // started
                ComicFixtures.COMIC_V1_2,
                ComicFixtures.COMIC_V1_3));

        progressRepository.save(ProgressFixtures.comicStarted(ComicFixtures.COMIC_V1_1));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findLastReadForVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext())
                .param("publisher", ComicFixtures.COMIC_V1_1.getPublisher())
                .param("series", ComicFixtures.COMIC_V1_1.getSeries())
                .param("volume", ComicFixtures.COMIC_V1_1.getVolume()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.title").value(ComicFixtures.COMIC_V1_1.getTitle()));
    }

    @Test
    public void findLastReadForUnstartedVolume() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                ComicFixtures.COMIC_V1_1,
                ComicFixtures.COMIC_V1_2,
                ComicFixtures.COMIC_V1_3));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findLastReadForVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext())
                .param("publisher", ComicFixtures.COMIC_V1_1.getPublisher())
                .param("series", ComicFixtures.COMIC_V1_1.getSeries())
                .param("volume", ComicFixtures.COMIC_V1_1.getVolume()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.title").value(ComicFixtures.COMIC_V1_1.getTitle()));
    }

    @Test
    public void findLastReadForVolumeWithMixedState() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                ComicFixtures.COMIC_V1_1,   // read
                ComicFixtures.COMIC_V1_2,
                ComicFixtures.COMIC_V1_3)); // read

        progressRepository.saveAll(Arrays.asList(
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V1_1),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V1_3)));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findLastReadForVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext())
                .param("publisher", ComicFixtures.COMIC_V1_1.getPublisher())
                .param("series", ComicFixtures.COMIC_V1_1.getSeries())
                .param("volume", ComicFixtures.COMIC_V1_1.getVolume()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.title").value(ComicFixtures.COMIC_V1_2.getTitle()));
    }

    @Test
    public void findLastReadForCompletedVolume() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                ComicFixtures.COMIC_V1_1,   // read
                ComicFixtures.COMIC_V1_2,   // read
                ComicFixtures.COMIC_V1_3)); // read

        progressRepository.saveAll(Arrays.asList(
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V1_1),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V1_2),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V1_3)));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findLastReadForVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext())
                .param("publisher", ComicFixtures.COMIC_V1_1.getPublisher())
                .param("series", ComicFixtures.COMIC_V1_1.getSeries())
                .param("volume", ComicFixtures.COMIC_V1_1.getVolume()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.title").value(ComicFixtures.COMIC_V1_1.getTitle()));
    }

    @Test
    public void findBookmarksMultipleVolumes() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                // Partly read volume at second issue
                ComicFixtures.COMIC_V1_1, // read
                ComicFixtures.COMIC_V1_2,
                ComicFixtures.COMIC_V1_3,

                // Unread volume
                ComicFixtures.COMIC_V2_1,
                ComicFixtures.COMIC_V2_2,
                ComicFixtures.COMIC_V2_3,

                // Partly read volume at third issue
                ComicFixtures.COMIC_V3_1, // read
                ComicFixtures.COMIC_V3_2, // read
                ComicFixtures.COMIC_V3_3));

        progressRepository.saveAll(Arrays.asList(
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V1_1, 3),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_1, 1),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_2, 2)));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findAllLastReadPerVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.comics.length()").value(2))
                .andExpect(jsonPath("$._embedded.comics[0].title").value(ComicFixtures.COMIC_V1_2.getTitle()))
                .andExpect(jsonPath("$._embedded.comics[1].title").value(ComicFixtures.COMIC_V3_3.getTitle()));
    }

    @Test
    public void findBookmarksFirstStarted() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                // Partly read volume at first issue
                ComicFixtures.COMIC_V1_1, // started
                ComicFixtures.COMIC_V1_2,
                ComicFixtures.COMIC_V1_3));

        progressRepository.save(ProgressFixtures.comicStarted(ComicFixtures.COMIC_V1_1));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findAllLastReadPerVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.comics.length()").value(1))
                .andExpect(jsonPath("$._embedded.comics[0].title").value(ComicFixtures.COMIC_V1_1.getTitle()));
    }

    @Test
    public void findBookmarksAllRead() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                // Completely read volume
                ComicFixtures.COMIC_V3_1,   // read
                ComicFixtures.COMIC_V3_2,   // read
                ComicFixtures.COMIC_V3_3)); // read

        progressRepository.saveAll(Arrays.asList(
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_1, 1),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_2, 2),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_3, 3)));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findAllLastReadPerVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.comics").doesNotExist());
    }

    @Test
    public void findBookmarksNoneRead() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                // Completely unread volume
                ComicFixtures.COMIC_V3_1,
                ComicFixtures.COMIC_V3_2,
                ComicFixtures.COMIC_V3_3));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findAllLastReadPerVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.comics").doesNotExist());
    }

    @Test
    public void findBookmarksLastStarted() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                // Almost read volume
                ComicFixtures.COMIC_V3_1, // read
                ComicFixtures.COMIC_V3_2, // read
                ComicFixtures.COMIC_V3_3));

        progressRepository.saveAll(Arrays.asList(
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_1, 1),
                ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_2, 2)));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findAllLastReadPerVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.comics.length()").value(1))
                .andExpect(jsonPath("$._embedded.comics[0].title").value(ComicFixtures.COMIC_V3_3.getTitle()));
    }

    @Test
    public void findBookmarksWithGaps() throws Exception {
        comicRepository.saveAll(Arrays.asList(
                // A volume with unread first issue
                ComicFixtures.COMIC_V3_1,
                ComicFixtures.COMIC_V3_2, // read
                ComicFixtures.COMIC_V3_3));

        progressRepository.save(ProgressFixtures.comicRead(ComicFixtures.COMIC_V3_2));

        mvc.perform(MockMvcRequestBuilders.get("/api/comics/search/findAllLastReadPerVolume")
                .with(authentication(OAuth2MockHelper.getOAuth2LoginAuthenticationToken()))
                .sessionAttr("scopedTarget.oauth2ClientContext", OAuth2MockHelper.getOauth2ClientContext()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.comics.length()").value(1))
                .andExpect(jsonPath("$._embedded.comics[0].title").value(ComicFixtures.COMIC_V3_1.getTitle()));
    }
}