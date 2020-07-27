(ns wcs.components.parser
  (:require
    [wcs.components.auto-resolvers :refer [automatic-resolvers]]
    [wcs.components.config :refer [config]]
    [wcs.components.connection-pools :as pools]
    [com.fulcrologic.rad.database-adapters.sql.plugin :as sql]
    [com.fulcrologic.rad.pathom :as pathom]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.blob :as blob]
    [wcs.components.blob-store :as bs]
    [wcs.components.save-middleware :as save]
    [wcs.components.delete-middleware :as delete]
    [mount.core :refer [defstate]]
    [wcs.model :refer [all-attributes]]
    [wcs.model.account :as account]
    [wcs.model.timezone :as timezone]
    [com.fulcrologic.rad.attributes :as rad.attr]
    [wcs.model.invoice :as invoice]))

(defstate parser
  :start
  (pathom/new-parser config
    [(rad.attr/pathom-plugin all-attributes)
     (form/pathom-plugin save/middleware delete/middleware)
     (sql/pathom-plugin (fn [_] {:production (:main pools/connection-pools)}))
     (blob/pathom-plugin bs/temporary-blob-store {:files         bs/file-blob-store
                                                  :avatar-images bs/image-blob-store})]
    [automatic-resolvers
     form/resolvers
     (blob/resolvers all-attributes)
     account/resolvers
     invoice/resolvers
     timezone/resolvers]))
