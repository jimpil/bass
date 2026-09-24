(ns com.github.jimpil.bass.impl.finder
  (:require
    [com.github.jimpil.bass.impl.base2  :as base2]
    [com.github.jimpil.bass.impl.base8  :as base8]
    [com.github.jimpil.bass.impl.base16 :as base16]
    [com.github.jimpil.bass.impl.base32 :as base32]
    [com.github.jimpil.bass.impl.base58 :as base58]
    [com.github.jimpil.bass.impl.base64 :as base64]))

;; extensible implementation finder
(defmulti encoder-decoder identity)
(defmethod encoder-decoder :base2       [_] `[base2/encode  base2/decode])
(defmethod encoder-decoder :base8       [_] `[base8/encode  base8/decode])
(defmethod encoder-decoder :base16      [_] `[base16/encode base16/decode])
(defmethod encoder-decoder :base32      [_] `[base32/encode base32/decode])
(defmethod encoder-decoder :base58      [_] `[base58/encode base58/decode])
(defmethod encoder-decoder :base64      [_] `[base64/encode base64/decode])
(defmethod encoder-decoder :base16/uc   [_] `[(partial base16/encode :upper) (partial base16/decode :upper)])
(defmethod encoder-decoder :base64/url  [_] `[(partial base64/encode :url)   (partial base64/decode :url)])
(defmethod encoder-decoder :base64/mime [_] `[(partial base64/encode :mime)  (partial base64/decode :mime)])


