```
 _                   
| |                  
| |__   __ _ ___ ___ 
| '_ \ / _` / __/ __|
| |_) | (_| \__ \__ \
|_.__/ \__,_|___/___/
                     
```

## What

A tiny (1 macro) Clojure library for encoding/decoding data to/from various numerical bases, 
with emphasis on correctness, ergonomics, and to some extent, performance:

- Base2  (aka binary): implemented using `java.lang.Integer`  
- Base8  (aka octal): implemented using `java.lang.Integer`
- Base16 (aka hex): passthrough to `java.util.HexFormat`  
- Base32 (standard): implemented from scratch
- Base58 (per Bitcoin alphabet): implemented using `java.lang.BigInteger`, or passthrough to `org.apache.commons.codec.binary.Base58` if found on the classpath
- Bse64: passthrough to `java.util.Base64`

## Why 

### Base2 & Base8
When dealing with base16 or base64, the JDK has you covered - it provides facilities for extremely fast (and correct)
data conversions. However, the facilities it offers for base2 & base8 are _number_ focused, rather than _data_ focused.
This has several implications - for example, leading zeroes don't matter when talking about a number, but they do matter
when dealing with a byte-array. This library takes care to **not** ignore leading zeroes from base2 & base8. 
See the performance section, for more on how they perform.

### Base58
Facilities for base58 simply don't exist in the JDK. The implementation provided here is significantly faster than  
anything I could find online (in Clojure), and yet, it is the slowest base within this library.
See the performance section, for more on how it performs.

#### Note 

If `commons-codec` is already on your classpath, you need not to worry about this base being slow - 
`org.apache.commons.codec.binary.Base58` will be used, and its performance is on par with Java's native conversions 
(e.g. `HexFormat`). See `base58.clj` for how this done.

### Why not x?

See `Alternatives` section at the end.

### But why macro-based?
The most widely used bases for real (production) systems, are of course, base16 & base64, and as mentioned earlier, 
the native JDK classes for those offer _hilarious_ performance (see performance section for more). Putting these calls
behind an abstraction, puts you in a situation where the dispatch logic may match, or even outweigh the actual conversion! 
If that sounds downright impossible/unbelievable, please do check the performance section! 
The `with-base` macro expands to a single function call (i.e. the _bottom_ encoder/decoder), and therefore incurs no cost.
 
From an ergonomics and debugging perspective, it's rather nice to be able to pass in a body of code, 
rather than a concrete value. It _may_ save you the trouble of creating unnecessary `let` expressions, 
or even no-name `_` bindings (for debug printouts).

Finally, the necessity that encoders be able to deal with `java.nio.ByteBuffer` (see the `Nuisance` section below), 
turns out to be quite the convenience (from an api consumer perspective).

## How

If it's not obvious by now, the following is the entire API.

### com.github.jimpil.bass.core/with-base [base op & body]
 
You must provide the base identifier & the operation to perform (`:encode` VS `:decode`) as literals,
followed by some body of code. The following identifiers are recognised:

- `:base2` or `"2"` or `2`
- `:base8` or `"8"` or `8`
- `:base16/lc` or `:base16` or `"16"` or `16` (lower-case)
- `:base16/uc` (upper-case)
- `:base32` or `"32"` or `32` (upper-case)
- `:base58` or `"58"` or `58`
- `:base64/std` or `:base64` or `"64"` or `64`
- `:base64/url`
- `:base64/mime`

Example: 

```clj
(require '[com.github.jimpil.bass.core :refer [with-base])

(let [input (byte-array [0 -1])
      roundtripped (->> input
                        (with-base 2  :encode)
                        (with-base 2  :decode)
                        (with-base 8  :encode)
                        (with-base 8  :decode)
                        (with-base 32 :encode)
                        (with-base 32 :decode)
                        (with-base 58 :encode)
                        (with-base 58 :decode))]
  (= (seq input) 
     (seq roundtripped))) ;; => true
```

### Nuisance

In order to avoid copying, the return type of decoders is not always consistent. 
They _may_ choose to return ByteBuffer instead of byte-array, which means that
encoders _must_ be able to deal with either (otherwise round-tripping breaks).

#### Input

Decoders always expect a String, whereas encoders can consume byte-array or `java.nio.ByteBuffer`.
Strings must be **well** (must adhere to alphabet) & **fully** formed - e.g. binary/octal String length must 
be a multiple of 8/3 respectively. Pad left or right, according to your needs, but do remember that this library 
assumes Big-Endian (i.e. MSB to the left).

#### Output

Encoders always return String, and decoders typically return `byte-array`. 
The implementation of base58 in this library (not `commons-codec`) _may_ return `java.nio.ByteBuffer`. 
This is in an attempt to avoid copying, and although certainly a compromise, I would argue that it's a small one, 
because any downstream code that is able to consume a byte-array, can trivially be ported/adapted to consume a 
ByteBuffer instead (i.e. both loops look fairly similar). If you'd rather have consistency over performance, 
the simplest thing you can do is to define your own wrapper. For example, let's say that you want an base58 decoder 
which _always_ returns byte-array:  

```clj
(require '[com.github.jimpil.bass.util :refer [buffer->bytes]
         '[com.github.jimpil.bass.core :refer [with-base])

(defn my-base8-decode 
  ^bytes [x]
  (buffer->bytes (with-base 58 :decode x)))
```
As always, before doing this, think about whether you actually need it. Will you be using base58? 
If yes, is `commons-codec` already on your classpath? If yes, then there is effectively no decoder 
that will return `ByteBuffer`. 

The other thing you can do is to assume byte-array, and look for reflection warnings. 
Because `with-base` compiles down to a single function call, if you're hitting a decoder with a non type-hinted return, 
your downstream code is bound to be reflective. 


### Custom bases 

There is no path for extending this library with new bases, but there is a path
for replacing the existing encoder/decoder implementation(s) that `with-base` will use.
This may be useful in situations where perhaps, you are not happy with a particular 
encoder/decoder pair (e.g. base58 is too slow for your liking), and you want to 
provide your own implementations. You do this via the `com.github.jimpil.bass.impl/roundtrip`
multi-method. 

The caveat here is that the implementations are looked up at compile-time.
This basically means that you need to ensure that your `defmethod` is evaluated before 
`with-base` usages are macro-expanded! The safest way to do that is to put it in a namespace,
which is then required by the namespace(s) that use `with-base`.

#### Attention! 

The above is just an example. Before you go ahead and replace the (admittedly slow) base58 impl with the ones 
from `commons-codec`, remember that, if `commons-codec` is detected on the classpath, this is done  
automatically (i.e. the encode/decode functions in `base58.clj` will delegate to the highly optimised 
`org.apache.commons.codec.binary.Base58` class).

## Performance

The measurements below were taken with `criterium` on a 2016 Macbook Pro (Intel i7 - 2.8 GHz), 
against Java 25 & Clojure 1.12.3. The expressions used to produce them can be found at the bottom of the
`core.clj` (the comment section). As you can see, the native Java methods (base16 & base64) are just ridiculous 
in terms of performance.

### Encode 1KB (i.e. 1024 bytes -> String)

| Base | Input  | Duration |
|:-----|:------:|---------:|
| 2    | array  |    13 µs |
| 2    | buffer |    15 µs |
| 8    | array  |    11 µs |
| 8    | buffer |    13 µs |
| 32   | array  |     8 µs |
| 32   | buffer |    13 µs |
| 58   | array  |   546 µs |
| 58   | buffer |   559 µs |

Just for laughs, here are the native Java numbers:

| Base       | Input | Duration |
|:-----------|:-----:|---------:|
| 16         | array |     2 µs |
| 58-commons | array |     2 µs |
| 64         | array |   737 ns |

### Decode 1KB (i.e. String -> 1024 bytes)

| Base | Input  | Duration |
|:-----|:------:|---------:|
| 2    | String |    48 µs |
| 8    | String |    37 µs |
| 32   | String |    54 µs |
| 58   | String |   129 µs |


#### TL;DR 

All encoders/decoders need _well under_ 20/60 micro-seconds respectively, to process 
1KB of data (on this ancient laptop). My base58 implementation is the outlier which is more expensive.
The one from `commons-codec` is _much_ faster.

## Alternatives

### commons-codec 

Apache [commons-codec](https://github.com/apache/commons-codec) is the obvious one - 
in fact this library started off as wrapper around it.
Unfortunately, I quickly realised that the `BinaryCodec` class (i.e. base2) treats the entire array in 
Little-Endian byte order, even though the bits within each byte are read in Big-Endian bit order. 
I find that this mixed-endian approach contradicts how humans read strings, and how most network protocols 
handle binary data. Considering that base16 & base64 are already covered in modern JDKs, and the fact that its `Base32` 
class is only marginally faster than my implementation, the only class that I would realistically end up using would 
be the `Base58` one. In fact, as mentioned multiple times by now, this class is indeed being used, if present 
on the classpath.

### buddy-core 

[buddy-core](https://github.com/funcool/buddy-core) depends on `commons-codec` for its base encoding/decoding needs, 
so everything mentioned above applies, not to mention, its scope and purpose are quite different 
(e.g. no direct support for binary, octal, base32 & base58). Finally, it does rely on  
protocol-dispatch (e.g. `buddy.core.codecs/IByteArray`), something which I am trying to avoid.  

### alphabase 
I discovered [mvxcvi/alphabase](https://github.com/greglook/alphabase) recently, and I have to say, there are many things I like about it. 
For starters, it is very similar in scope & spirit, it doesn't introduce any abstractions,
and it works on both clj & cljs! Unfortunately, some of the implementations it provides are _really_ slow. 
For example:

- base2 encoding/decoding is ~35x/5x slower respectively
- base8 encoding/decoding is ~630x/24x slower respectively
- base58 encoding/decoding is ~10x/6x slower respectively (compared to my 'slow' impl)

On the other hand, its base32 encoding/decoding is ~3x/7x faster respectively, and is of course, written in Java.
Finally, its base16 implementation (also pure Java) is essentially comparable to `java.util.HexFormat` (marginally slower).

## License

Copyright © 2026 Dimitrios Piliouras

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
