/*
 * ToroDB
 * Copyright © 2014 8Kdata Technology (www.8kdata.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.torodb.mongodb.commands.impl.admin;

import com.torodb.core.exceptions.user.UserException;
import com.torodb.mongodb.commands.impl.ExclusiveWriteTorodbCommandImpl;
import com.torodb.mongodb.commands.signatures.admin.RenameCollectionCommand.RenameCollectionArgument;
import com.torodb.mongodb.core.ExclusiveWriteMongodTransaction;
import com.torodb.mongowp.ErrorCode;
import com.torodb.mongowp.Status;
import com.torodb.mongowp.commands.Command;
import com.torodb.mongowp.commands.Request;
import com.torodb.mongowp.commands.tools.Empty;

public class RenameCollectionImplementation implements
    ExclusiveWriteTorodbCommandImpl<RenameCollectionArgument, Empty> {

  @Override
  public Status<Empty> apply(Request req,
      Command<? super RenameCollectionArgument, ? super Empty> command,
      RenameCollectionArgument arg, ExclusiveWriteMongodTransaction context) {
    try {
      if (arg.isDropTarget()) {
        context.getTorodTransaction().dropCollection(arg.getToDatabase(), arg.getToCollection());
      }

      context.getTorodTransaction().renameCollection(arg.getFromDatabase(), arg.getFromCollection(),
          arg.getToDatabase(), arg.getToCollection());
    } catch (UserException ex) {
      return Status.from(ErrorCode.COMMAND_FAILED, ex.getLocalizedMessage());
    }

    return Status.ok();
  }

}
