package dev.tim9h.lastfm.service.controller;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.tim9h.lastfm.service.entity.AgeBean;

@RestController
@RequestMapping("/lastfm")
public class LastFmController {

	private static final Logger LOGGER = LogManager.getLogger(LastFmController.class);

	public LastFmController() {
	}

	@GetMapping("/age/{username}")
	public AgeBean getMusicalAge(@PathVariable("username") String username) {
		LOGGER.debug(() -> "Getting age for " + username);
		return new AgeBean(13, Arrays.asList(Pair.of(1, 2)));
	}

}