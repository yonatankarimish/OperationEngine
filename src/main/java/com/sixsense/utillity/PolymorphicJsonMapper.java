package com.sixsense.utillity;

import com.sixsense.model.commands.ICommand;
import com.sixsense.model.logic.IResolvable;
import com.sixsense.model.pipes.AbstractOutputPipe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
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
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            //.activateDefaultTyping(baseTypeValidator, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE) //keep this commented out in the meantime to avoid exposing/requiring full type data for pojo <=> json conversions
            .build();
    }

    public static String serialize(Object pojo) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pojo);
    }

    public static <T> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(json, clazz);
    }
}
