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
  // Admin Messages
  ADMIN_REALOAD_SUCCESSFUL("reloadSuccessful"),
  ADMIN_REALOAD_NOT_SUCCESSFUL("reloadNotSuccessful"),
  // Error Messages
  ERROR_NOT_A_PLAYER("notAPlayer"),
  ERROR_NNO_BLOCK_IN_HAND("noBlockInHand"),
  // Limit Messages
  LIMIT_LIMIT_REACHED("limitReached"),
  LIMIT_BLOCK_STATS_PADDING("blockStatsPadding"),
  LIMIT_BLOCK_STATS_TITLE("blockStatsTitle"),
  LIMIT_BLOCK_STATS("blockStats"),
  LIMIT_NO_LIMIT("noLimit");

  @Getter private final String stringPath;

  public Text getMessage() {
    return getMessage(null);
  }

  public Text getMessage(Map<String, String> replacements) {
    final String message = AuraBlockLimit.getTranslator().translateWithFallback(this, replacements);

    return TextSerializers.FORMATTING_CODE.deserialize(message);
  }
}
