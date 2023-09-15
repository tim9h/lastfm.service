package dev.tim9h.lastfm.service.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.musicbrainz.MBWS2Exception;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.umass.lastfm.User;
import dev.tim9h.lastfm.service.entity.AgeBean;

@Service
public class LastfmService {

	private static final Logger LOGGER = LogManager.getLogger(LastfmService.class);

	@Value("${lastfm.apikey}")
	private String apiKey;

	public AgeBean getMusicalAge(String username) {
		LOGGER.debug(() -> "Getting age for " + username);

		var albums = User.getTopAlbums(username, apiKey);
		var history = new ArrayList<Pair<Integer, Integer>>();

		albums.forEach(album -> {
			try {
				var release = MusicbrainzService.getReleaseYear(album.getMbid(), album.getName(), album.getArtist(),
						album.getPlaycount());
				if (release == null) {
					LOGGER.warn(() -> String.format("Release year not found for %s - %s", album.getArtist(),
							album.getName()));
					return;
				}
				history.add(Pair.of(release, Integer.valueOf(album.getPlaycount())));
			} catch (MBWS2Exception e) {
				LOGGER.warn(() -> String.format("Unable to find release %s (%s - %s)", album.getMbid(),
						album.getArtist(), album.getName()), e);
			}
		});

		var avg = history.stream().collect(averagingWeighted(Pair::getLeft, Pair::getRight));
		var age = Integer.valueOf(Year.now().getValue() - avg.intValue());

		LOGGER.info(() -> "Finished collecting release dates");
		return new AgeBean(Integer.valueOf(avg.intValue()), age, history);
	}

	private static <T> Collector<T, ?, Double> averagingWeighted(ToDoubleFunction<T> valueFunction,
			ToIntFunction<T> weightFunction) {
		class Box {
			double num = 0;
			long denom = 0;
		}
		return Collector.of(Box::new, (b, e) -> {
			b.num += valueFunction.applyAsDouble(e) * weightFunction.applyAsInt(e);
			b.denom += weightFunction.applyAsInt(e);
		}, (b1, b2) -> {
			b1.num += b2.num;
			b1.denom += b2.denom;
			return b1;
		}, b -> Double.valueOf((b.num / b.denom)));
	}

}
