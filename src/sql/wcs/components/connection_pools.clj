(ns wcs.components.connection-pools
  (:require
    [mount.core :refer [defstate]]
    [wcs.model :refer [all-attributes]]
    [wcs.components.config :refer [config]]
    [com.fulcrologic.rad.database-adapters.sql.result-set :as rs]
    [com.fulcrologic.rad.database-adapters.sql.connection :as pools]))

(defstate connection-pools
  :start
  (do
    (rs/coerce-result-sets!)
    (pools/create-connection-pools! config all-attributes))
  :stop
  (pools/stop-connection-pools! connection-pools))

