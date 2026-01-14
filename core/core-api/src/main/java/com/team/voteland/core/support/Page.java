package com.team.voteland.core.support;

import java.util.List;

public record Page<T>(List<T> contents, Boolean hasNext) {

}