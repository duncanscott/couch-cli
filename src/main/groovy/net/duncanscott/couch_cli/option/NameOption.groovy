package net.duncanscott.couch_cli.option

/**
 * Created by Duncan on 2/2/14.

 Project: couch-cli

 couch-cli command line interface to Apache CouchDb databases
 Copyright (C) 2014  Duncan Scott

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.


 Duncan Scott
 PostalAnnex
 785 E2 Oak Grove Road #229
 Concord, CA 94518-3617-US

 email:duncan@duncanscott.net
 */

import net.duncanscott.couch_cli.action.AbstractAction
import net.duncanscott.couch_cli.client.CouchClientErrorMessage
import org.apache.commons.cli.Option

/**
 * Created by Duncan on 2/1/14.
 */
class NameOption extends BaseOption {

    NameOption(AbstractAction action) {
        super(action)
        cliBuilder.n(longOpt:'name', args: Option.UNLIMITED_VALUES, required:true, 'names of databases')
    }

    Set<String> getNames() {
        Set<String> nameArgs = []
        if (optionAccessor.name) {
            nameArgs += optionAccessor.names
        }
        return nameArgs
    }

}
