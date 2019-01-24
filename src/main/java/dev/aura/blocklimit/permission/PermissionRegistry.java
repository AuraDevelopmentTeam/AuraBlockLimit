package dev.aura.blocklimit.permission;

import dev.aura.blocklimit.AuraBlockLimit;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;

@RequiredArgsConstructor
public class PermissionRegistry {
  public static final String BASE = AuraBlockLimit.ID;
  public static final String COMMAND = BASE + ".command";

  private final AuraBlockLimit plugin;
  private final PermissionService service =
      Sponge.getServiceManager().provide(PermissionService.class).get();

  public void registerPermissions() {
    registerPermission(BASE, PermissionDescription.ROLE_ADMIN);
    registerPermission(COMMAND, "Permission for all commands", PermissionDescription.ROLE_ADMIN);
  }

  private Builder getBuilder() {
    return service.newDescriptionBuilder(plugin);
  }

  private void registerPermission(String permission, String role) {
    registerPermission(permission, null, role);
  }

  private void registerPermission(String permission, @Nullable String description, String role) {
    getBuilder()
        .id(permission)
        .description((description == null) ? Text.of() : Text.of(description))
        .assign(role, true)
        .register();
  }
}
