package com.team.voteland.docs;

import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;
import org.springframework.restdocs.operation.preprocess.Preprocessors;

public class RestDocsUtils {

    public static OperationResponsePreprocessor responsePreprocessor() {
        return Preprocessors.preprocessResponse(Preprocessors.prettyPrint());
    }

}