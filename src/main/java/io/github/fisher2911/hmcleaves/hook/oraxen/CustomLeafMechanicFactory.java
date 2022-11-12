package io.github.fisher2911.hmcleaves.hook.oraxen;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class CustomLeafMechanicFactory extends MechanicFactory {

    public CustomLeafMechanicFactory(ConfigurationSection section) {
        super(section);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new CustomLeafMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

}
