package com.team.voteland.client.example;

import com.team.voteland.client.example.model.ExampleClientResult;

record ExampleResponseDto(String exampleResponseValue) {
    ExampleClientResult toResult() {
        return new ExampleClientResult(exampleResponseValue);
    }
}
