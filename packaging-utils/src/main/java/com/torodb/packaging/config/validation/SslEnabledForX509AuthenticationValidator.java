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

package com.torodb.packaging.config.validation;

import com.torodb.packaging.config.model.protocol.mongo.AbstractReplication;
import com.torodb.packaging.config.model.protocol.mongo.AbstractShardReplication;
import com.torodb.packaging.config.model.protocol.mongo.AuthMode;
import com.torodb.packaging.config.model.protocol.mongo.Ssl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SslEnabledForX509AuthenticationValidator implements
    ConstraintValidator<SslEnabledForX509Authentication, AbstractReplication<?>> {

  @Override
  public void initialize(SslEnabledForX509Authentication constraintAnnotation) {
  }

  @Override
  public boolean isValid(AbstractReplication<?> value, ConstraintValidatorContext context) {
    if (value != null) {
      if (value.getShardList().isEmpty()) {
        if (value.getAuth().getMode().value() == AuthMode.x509) {
          Ssl ssl = value.getSsl();
          if (!ssl.getEnabled().value() || ssl.getKeyStoreFile() == null 
              || ssl.getKeyPassword() == null) {
            return false;
          }
        }
      } else {
        for (AbstractShardReplication shard : value.getShardList()) {
          if (shard.getAuth().getMode().mergeValue( 
              value.getAuth().getMode()) == AuthMode.x509) {
            Ssl shardSsl = shard.getSsl();
            Ssl commonSsl = value.getSsl();
            if (!shardSsl.getEnabled().mergeValue(commonSsl.getEnabled()) 
                || shardSsl.getKeyStoreFile().mergeValue(commonSsl.getKeyStoreFile()) == null
                || shardSsl.getKeyPassword().mergeValue(commonSsl.getKeyPassword()) == null) {
              return false;
            }
          }
        }
      }
    }

    return true;
  }
}
