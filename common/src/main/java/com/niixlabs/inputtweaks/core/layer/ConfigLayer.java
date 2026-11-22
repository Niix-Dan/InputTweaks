package com.niixlabs.inputtweaks.core.layer;

import com.niixlabs.inputtweaks.core.model.BindOrigin;
import com.niixlabs.inputtweaks.core.model.ResolvedBind;

public interface ConfigLayer {
    BindOrigin origin();

    int order();

    void apply(String bindId, ResolvedBind.Builder builder);
}
