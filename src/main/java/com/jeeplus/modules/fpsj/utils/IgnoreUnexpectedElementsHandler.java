package com.jeeplus.modules.fpsj.utils;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

public class IgnoreUnexpectedElementsHandler implements ValidationEventHandler {
	@Override
	public boolean handleEvent(ValidationEvent event) {
		return event.getMessage().startsWith("unexpected element (");
	}
}
