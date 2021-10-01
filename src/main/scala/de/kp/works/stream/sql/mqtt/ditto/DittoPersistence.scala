package de.kp.works.stream.sql.mqtt.ditto
/*
 * Copyright (c) 2020 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author Stefan Krusche, Dr. Krusche & Partner PartG
 *
 */

import org.rocksdb.{Options, RocksDB}
import java.util.Objects

object DittoPersistence {

  var persistence: RocksDB = _

  def getOrCreate(path: String): RocksDB = {

    if (Objects.isNull(persistence)) {
      RocksDB.loadLibrary()
      persistence = RocksDB.open(new Options().setCreateIfMissing(true), path)
    }

    persistence

  }

  def close(): Unit = {

    if (!Objects.isNull(persistence)) {
      persistence.close()
      persistence = null
    }

  }

}
