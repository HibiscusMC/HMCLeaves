package com.hibiscusmc.hmcleaves.common;

import com.hibiscusmc.hmcleaves.common.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.common.database.LeavesDatabase;
import com.hibiscusmc.hmcleaves.common.world.LeavesWorldManager;

import java.util.logging.Level;

public interface HMCLeaves {

    LeavesConfig<?> leavesConfig();

    LeavesWorldManager worldManager();

    LeavesDatabase database();

    void log(Level level, String message);

}
