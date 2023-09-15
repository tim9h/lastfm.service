package dev.tim9h.lastfm.service.entity;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public record AgeBean(Integer year, Integer age, List<Pair<Integer, Integer>> ages) {

}