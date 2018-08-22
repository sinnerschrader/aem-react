package com.sinnerschrader.aem.react.node;

import com.sinnerschrader.aem.react.api.Sling.EditDialog;

public interface EditDialogLoader {


	public EditDialog load(String path, String resourceType);

}
