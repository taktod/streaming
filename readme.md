# rtmpストリームデータからいろいろするプロジェクト

rtmpをしゃべるサーバーに接続したストリームをflazrでひっぱってきて、
xuggleで適当にコンバートをかけて、いろいろしたいと思います。

# やりたいこと。

httpLiveStreamingとhttpTakStreaming、あとはwebmのストリーミングに対応したいと思っています。
変換用のxuggleは最新のものを用意してください。(vpxのコンバートができないため。)

httpLiveStreamingとhttpTakStreamingでは、複数同時コンバートを実施したいとおもいます。
ユーザーの帯域によって、自動的にクオリティやサイズを変更することができる。みたいなのを目指します。
また、httpTakStreamingでは、いままでのhttpに加えてrtmpやrtmfpによるストリーミングも目指したいとおもいます。

webMのマトリョーシカのstreamingからwebmのライブストリーミングも実施したいと思っています。
これでスマートフォンでは、videoタグでストリーミング再生できるはず。

# 目標とする動作

とりあえずこのプログラムでは、各セグメントを作成して、適当なところに配置することを目標とします。
bitrateやsizeの違うストリームを同時に作成することが、目標。

# パッケージ詳細

com.ttProject.flazr.ex
 flazrの動作拡張

com.ttProject.flazr
 flazrのダウンロード動作拡張

com.ttProject.xuggle
 xuggleの変換動作

com.ttProject.xuggle.flv
 flvの入力データ管理

com.ttporject.streaming.*
 それぞれの出力データの管理


# 更新日記

mpegts、flv、webmの各データの出力は一応できました。

各ManagerのHandler作成部のパス[~/]ではじめるようにしてますが、それぞれのフルパスに書き換える必要があります。

また、flashMediaServerにFMEでデータを送信したときに、transcodeWriterのaggregate時の動作を解いてやる必要があります。
