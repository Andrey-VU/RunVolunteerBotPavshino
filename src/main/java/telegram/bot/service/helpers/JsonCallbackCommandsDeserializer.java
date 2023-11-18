package telegram.bot.service.helpers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import telegram.bot.service.enums.Callbackcommands;

import java.io.IOException;

public class JsonCallbackCommandsDeserializer extends StdDeserializer<Callbackcommands> {

    protected JsonCallbackCommandsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Callbackcommands deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        return Callbackcommands.valueOf(jsonParser.getText());
    }
}
