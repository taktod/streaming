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
 flvのデータ入力まわりもここで管理します。
com.ttProject.httpLiveStreaming
com.ttProject.httpTakStreaming
com.ttProject.httpWebMStreaming

