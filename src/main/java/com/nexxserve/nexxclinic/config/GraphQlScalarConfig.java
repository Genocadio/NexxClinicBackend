package com.nexxserve.nexxclinic.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.web.multipart.MultipartFile;

@Configuration
public class GraphQlScalarConfig {

    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> {
            wiringBuilder.scalar(ExtendedScalars.Json);
            wiringBuilder.scalar(uploadScalar());
        };
    }

    @Bean
    GraphQLScalarType uploadScalar() {
        return GraphQLScalarType.newScalar()
                .name("Upload")
                .description("A file upload scalar")
                .coercing(new Coercing<>() {
                    @Override
                    public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof MultipartFile file) {
                            return file.getOriginalFilename();
                        }
                        throw new CoercingSerializeException("Upload serialization not supported");
                    }

                    @Override
                    public Object parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof MultipartFile) {
                            return input;
                        }
                        throw new CoercingParseValueException("Expected a MultipartFile");
                    }

                    @Override
                    public Object parseLiteral(Object input) throws CoercingParseLiteralException {
                        throw new CoercingParseLiteralException("Upload literal not supported");
                    }
                })
                .build();
    }
}
