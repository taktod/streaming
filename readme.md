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

各ManagerのHandler作成部のパス[/home/xxxx/]ではじめるようにしてますが、環境にあわせて適宜書き換えてください。

また、flashMediaServerにFMEでデータを送信したときに、transcodeWriterのaggregate時の動作を解いてやる必要があります。

h.264の入力ソースを使うと、動作にエラーが発生することがあったのですが、対処しました。

2012/10/03
多重コンバート動作成功
http://poepoemix.blogspot.jp/2012/10/blog-post.html

2012/10/07
入力エラー(AVC nal sizeなんとかというエラー)対処
http://poepoemix.blogspot.jp/2012/10/blog-post_7.html
http://poepoemix.blogspot.jp/2012/10/blog-post_4308.html

2012/10/08
コンテナ作成部、エンコード部マルチスレッド化
http://poepoemix.blogspot.jp/2012/10/blog-post_4868.html
(メモ)ffmpegの動作部分がシングルスレッドになっているのか、マルチスレッド化してもたいして速くなりませんでした。

2012/10/08
flvの生ストリーム出力動作をtakに追加

2012/10/14
webmとmpegtsの分割用のプログラムを書いてみた。

2012/10/21
flvのデコード処理がいまいちだったので、自力でIPacketをつくることにしてみました。

2012/10/26
いろんなflvについて調べてみた。
FlashMediaServerについているvodのsample.flvについてちょっと面白いことがわかった。どうやらaudioが先にはじまるみたいで、かつvideoの開始パケットがtimestamp=0からはじまっていなかった。
このflvでも問題なくxuggleで変換かけれた。
FlashMediaServerからひっぱってくるときのaggregateのメッセージ変換がまちがってるとしかおもえないんですよね・・・

2012/10/28
ブロガーの記事にも書いたけど
http://poepoemix.blogspot.jp/2012/10/flashmediaserver_27.html
FlashMediaServerが吐くデータについて調査してみた。
とりあえず生成Flvファイルの安定度がぱねぇ・・・
xuggleの変換については、やっぱりH.264データを長時間変換してるとIContainer.readNextPacketが壊れるっぽいので、やはり自力のFlvCustomReaderを利用する方向ですすめようと思う。
このプログラム・・・企業とかで使うには非常にアレな感じになっちゃいましたね。

あと、ずっと気づかなかったんですが、com.ttProject.streaming.xxx.XXXHandlerの動作・・・
出力コンテナの開き直し実行時のファイルの開き直しが成ってなかった・・・orz
音声動作追加したら急に動作しなくなったからプログラムのバグかとおもっちゃったよ。ちくしょう。
