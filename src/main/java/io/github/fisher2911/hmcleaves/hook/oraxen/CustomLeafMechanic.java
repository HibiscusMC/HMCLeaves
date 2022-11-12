package io.github.fisher2911.hmcleaves.hook.oraxen;

import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;

public class CustomLeafMechanic extends Mechanic {

    private final String leafItemId;

    public CustomLeafMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(PDCUtil.ITEM_KEY, PersistentDataType.STRING, section.getString("leaf-id"))
        );
        this.leafItemId = section.getString("leaf-id");
    }

    public String getLeafItemId() {
        return leafItemId;
    }

}
