package dev.aura.blocklimit.message;

import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.lib.messagestranslator.Message;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

@RequiredArgsConstructor
public enum PluginMessages implements Message {
  ADMIN_REALOAD_SUCCESSFUL("reloadSuccessful");

  @Getter private final String stringPath;

  public Text getMessage() {
    return getMessage(null);
  }

  public Text getMessage(Map<String, String> replacements) {
    String message = AuraBlockLimit.getTranslator().translateWithFallback(this);

    if (replacements != null) {
      for (Map.Entry<String, String> replacement : replacements.entrySet()) {
        message = message.replaceAll('%' + replacement.getKey() + '%', replacement.getValue());
      }
    }

    return TextSerializers.FORMATTING_CODE.deserialize(message);
  }
}
