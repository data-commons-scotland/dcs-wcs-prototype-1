(ns wcs.model
  (:require
    [wcs.model.timezone :as timezone]
    [wcs.model.account :as account]
    [wcs.model.item :as item]
    [wcs.model.invoice :as invoice]
    [wcs.model.line-item :as line-item]
    [wcs.model.address :as address]
    [wcs.model.category :as category]
    [wcs.model.file :as m.file]
    [wcs.model.sales :as sales]
    [com.fulcrologic.rad.attributes :as attr]))

(def all-attributes (vec (concat
                           account/attributes
                           address/attributes
                           category/attributes
                           item/attributes
                           invoice/attributes
                           line-item/attributes
                           m.file/attributes
                           sales/attributes
                           timezone/attributes)))

(def all-attribute-validator (attr/make-attribute-validator all-attributes))
