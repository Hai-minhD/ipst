/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.server.attributes;

import be.pepite.dataserver.api.FunctionalColumn;

/**
 * Created with IntelliJ IDEA.
 * User: pduchesne
 * Date: 22/10/13
 * Time: 10:36
 * To change this template use File | Settings | File Templates.
 */
public class StrictlyPositive
    extends FunctionalColumn
{
    String attributeName;

    public StrictlyPositive(String attributeName) {

        //super(attributeName+"P", "( typeof "+attributeName + " !== 'undefined' && "+attributeName + " > 0 ? "+attributeName + " : null )");
        super(attributeName+"P", "( dbobj['"+attributeName + "'] !== undefined &&  dbobj['"+attributeName + "'] > 0 ? dbobj['"+attributeName + "'] : null )");
        this.attributeName = attributeName;
    }

    @Override
    public String[] getRequiredInputs() {
        return new String[] {attributeName};
    }
}
