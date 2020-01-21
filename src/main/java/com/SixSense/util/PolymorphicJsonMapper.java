package com.SixSense.util;

import com.SixSense.data.commands.ICommand;
import com.SixSense.data.logic.IResolvable;
import com.SixSense.data.pipes.AbstractOutputPipe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

public class PolymorphicJsonMapper {
    private static final ObjectMapper mapper;

    static{
        PolymorphicTypeValidator baseTypeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(ICommand.class)
            .allowIfBaseType(IResolvable.class)
            .allowIfBaseType(AbstractOutputPipe.class)
            .build();

        mapper = JsonMapper.builder()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            //.activateDefaultTyping(baseTypeValidator, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE) //keep this commented out in the meantime to avoid exposing/requiring full type data for pojo <=> json conversions
            .build();
    }

    public static String serialize(Object pojo) throws JsonProcessingException {
        return mapper.writeValueAsString(pojo);
    }

    public static <T> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(json, clazz);
    }
}
