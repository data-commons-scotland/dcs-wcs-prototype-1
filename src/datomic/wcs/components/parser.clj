(ns wcs.components.parser
  (:require
    [wcs.components.auto-resolvers :refer [automatic-resolvers]]
    [wcs.components.blob-store :as bs]
    [wcs.components.config :refer [config]]
    [wcs.components.datomic :refer [datomic-connections]]
    [wcs.components.delete-middleware :as delete]
    [wcs.components.save-middleware :as save]
    [wcs.model :refer [all-attributes]]
    [wcs.model.account :as account]
    [wcs.model.invoice :as invoice]
    [wcs.model.timezone :as timezone]
    [wcs.ui.autocomplete :as autocomplete]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.pathom :as pathom]
    [mount.core :refer [defstate]]
    [wcs.model.sales :as sales]
    [wcs.model.item :as item]
    [com.wsscode.pathom.core :as p]
    [com.fulcrologic.rad.type-support.date-time :as dt]))

(defstate parser
  :start
  (pathom/new-parser config
    [(attr/pathom-plugin all-attributes)
     (form/pathom-plugin save/middleware delete/middleware)
     (datomic/pathom-plugin (fn [env] {:production (:main datomic-connections)}))
     (blob/pathom-plugin bs/temporary-blob-store {:files         bs/file-blob-store
                                                  :avatar-images bs/image-blob-store})
     {::p/wrap-parser
      (fn transform-parser-out-plugin-external [parser]
        (fn transform-parser-out-plugin-internal [env tx]
          ;; TASK: This should be taken from account-based setting
          (dt/with-timezone "America/Los_Angeles"
            (if (and (map? env) (seq tx))
              (parser env tx)
              {}))))}]
    [automatic-resolvers
     form/resolvers
     (blob/resolvers all-attributes)
     account/resolvers
     invoice/resolvers
     item/resolvers
     sales/resolvers
     timezone/resolvers
     autocomplete/resolvers]))
