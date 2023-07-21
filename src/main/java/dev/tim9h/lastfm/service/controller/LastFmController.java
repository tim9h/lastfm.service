package dev.tim9h.lastfm.service.controller;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.musicbrainz.MBWS2Exception;
import org.musicbrainz.controller.Release;
import org.musicbrainz.controller.ReleaseGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.umass.lastfm.Album;
import de.umass.lastfm.User;
import dev.tim9h.lastfm.service.entity.AgeBean;

@RestController
@RequestMapping("/lastfm")
public class LastFmController {

	private static final Logger LOGGER = LogManager.getLogger(LastFmController.class);

	@Value("${lastfm.apikey}")
	private String apiKey;

	@GetMapping("/age/{username}")
	public AgeBean getMusicalAge(@PathVariable("username") String username) {
		LOGGER.debug(() -> "Getting age for " + username);

		var albums = User.getTopAlbums(username, apiKey);
		var years = IntStream.builder();
		var history = new ArrayList<Pair<Integer, Integer>>();

		albums.forEach(album -> {
			try {
				var release = getReleaseYear(album);
				if (release == null) {
					LOGGER.warn(() -> String.format("Release year not found for %s - %s", album.getArtist(),
							album.getName()));
					return;
				}
				years.add(release.intValue());
				history.add(Pair.of(Integer.valueOf(album.getPlaycount()), release));

			} catch (MBWS2Exception e) {
				LOGGER.warn(() -> String.format("Unable to find release %s (%s - %s)", album.getMbid(),
						album.getArtist(), album.getName()), e);
			}
		});

		var avg = Integer.valueOf((int) years.build().average().orElse(-1));

		LOGGER.info(() -> "Finished collecting release dates");
		return new AgeBean(avg, history);
	}

	private static Integer getReleaseYear(Album album) throws MBWS2Exception {
		Integer release;
		if (StringUtils.isNotBlank(album.getMbid())) {
			var rel = new Release().lookUp(album.getMbid());
			if (StringUtils.isNotBlank(rel.getYear())) {
				release = Integer.valueOf(rel.getYear());
			} else {
				release = getReleaseYearFromReleaseGroup(album);
			}
		} else {
			release = getReleaseYearFromReleaseGroup(album);
		}
		LOGGER.info(() -> String.format("%s - %s (%s): %d scrobbles", album.getArtist(), album.getName(), release,
				Integer.valueOf(album.getPlaycount())));
		return release;
	}

	private static Integer getReleaseYearFromReleaseGroup(Album album) throws MBWS2Exception {
		LOGGER.debug(() -> "MBID not found for " + album.getName());
		var controller = new ReleaseGroup();
		controller.search(String.format("recording:\"%s\" && artist:\"%s\"", album.getName(), album.getArtist()));
		var list = controller.getFirstSearchResultPage();
		var optionalRG = list.stream().filter(result -> StringUtils.isNotBlank(result.getReleaseGroup().getYear()))
				.findFirst();
		if (optionalRG.isPresent()) {
			if (StringUtils.isNotBlank(optionalRG.get().getReleaseGroup().getYear())) {
				LOGGER.debug(() -> String.format("Using release date from release group for %s - %s", album.getArtist(),
						album.getName()));
				return Integer.valueOf(optionalRG.get().getReleaseGroup().getYear());
			} else {
				var optionalRelease = optionalRG.get().getReleaseGroup().getReleases().stream()
						.filter(x -> StringUtils.isNotBlank(x.getYear())).findFirst();
				if (optionalRelease.isPresent()) {
					LOGGER.debug(() -> String.format("Using release date from release for %s - %s", album.getArtist(),
							album.getName()));
					return Integer.valueOf(optionalRelease.get().getYear());
				} else {
					throw new MBWS2Exception(String.format("No release found with release year for %s - %s",
							album.getArtist(), album.getName()));
				}
			}
		} else {
			throw new MBWS2Exception(String.format("No release group found with release year for %s - %s",
					album.getArtist(), album.getName()));
		}
	}

}