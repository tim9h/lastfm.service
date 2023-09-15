package dev.tim9h.lastfm.service.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.musicbrainz.MBWS2Exception;
import org.musicbrainz.controller.Release;
import org.musicbrainz.controller.ReleaseGroup;
import org.springframework.stereotype.Service;

@Service
public class MusicbrainzService {

	private static final Logger LOGGER = LogManager.getLogger(MusicbrainzService.class);

	private MusicbrainzService() {
		//
	}

	public static Integer getReleaseYear(String id, String album, String artist, int playcount) throws MBWS2Exception {
		Integer release;
		if (StringUtils.isNotBlank(id)) {
			var rel = new Release().lookUp(id);
			if (StringUtils.isNotBlank(rel.getYear())) {
				release = Integer.valueOf(rel.getYear());
			} else {
				release = getReleaseYearFromReleaseGroup(album, artist);
			}
		} else {
			release = getReleaseYearFromReleaseGroup(album, artist);
		}
		LOGGER.info(
				() -> String.format("%s - %s (%s): %d scrobbles", artist, album, release, Integer.valueOf(playcount)));
		return release;
	}

	private static Integer getReleaseYearFromReleaseGroup(String name, String artist) throws MBWS2Exception {
		LOGGER.debug(() -> "MBID not found for " + name);
		var controller = new ReleaseGroup();
		controller.search(String.format("recording:\"%s\" && artist:\"%s\"", name, artist));
		var list = controller.getFirstSearchResultPage();
		var optionalRG = list.stream().filter(result -> StringUtils.isNotBlank(result.getReleaseGroup().getYear()))
				.findFirst();
		if (optionalRG.isPresent()) {
			if (StringUtils.isNotBlank(optionalRG.get().getReleaseGroup().getYear())) {
				LOGGER.debug(() -> String.format("Using release date from release group for %s - %s", artist, name));
				return Integer.valueOf(optionalRG.get().getReleaseGroup().getYear());
			} else {
				var optionalRelease = optionalRG.get().getReleaseGroup().getReleases().stream()
						.filter(x -> StringUtils.isNotBlank(x.getYear())).findFirst();
				if (optionalRelease.isPresent()) {
					LOGGER.debug(() -> String.format("Using release date from release for %s - %s", artist, name));
					return Integer.valueOf(optionalRelease.get().getYear());
				} else {
					throw new MBWS2Exception(
							String.format("No release found with release year for %s - %s", artist, name));
				}
			}
		} else {
			throw new MBWS2Exception(
					String.format("No release group found with release year for %s - %s", artist, name));
		}
	}

}
