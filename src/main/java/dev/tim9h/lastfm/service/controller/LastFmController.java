package dev.tim9h.lastfm.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.tim9h.lastfm.service.entity.AgeBean;
import dev.tim9h.lastfm.service.service.LastfmService;

@RestController
@RequestMapping("/lastfm")
public class LastFmController {

	@Autowired
	private LastfmService lfmService;

	@GetMapping("/age/{username}")
	public AgeBean getMusicalAge(@PathVariable("username") String username) {
		return lfmService.getMusicalAge(username);

	}

}