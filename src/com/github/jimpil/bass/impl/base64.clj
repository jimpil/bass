(ns com.github.jimpil.bass.impl.base64
  (:require
    [com.github.jimpil.bass.util :as util])
  (:import
    [java.util Base64 Base64$Decoder Base64$Encoder]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn encode
  "Encodes the provided byte-array <bs> in Base64.
   Returns String."
  (^String [bs]
   (encode :std bs))
  (^String [input-type bs]
   (let [^bytes bs (util/buffer->bytes bs)
         ^Base64$Encoder encoder
                   (case input-type
                     :url (Base64/getUrlEncoder)
                     :mime (Base64/getMimeEncoder)
                     (:std :default) (Base64/getEncoder))]
     (.encodeToString encoder bs))))

(defn decode
  "Decodes the provided String <s> from Base64.
   Returns byte-array."
  (^bytes [s]
   (decode :std s))
  (^bytes [input-type ^String s]
   (let [^Base64$Decoder decoder
         (case input-type
           :url     (Base64/getUrlDecoder)
           :mime    (Base64/getMimeDecoder)
           (:std :default) (Base64/getDecoder))]
     (.decode decoder s))))
