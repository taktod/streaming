# rtmpのストリームデータからいろいろするプロジェクト

red5サーバーに流されたデータを元に・・・
もしくはflazrで取得したデータを元に・・・
httpLiveStreaming(iOS対応),
jpegMp3Streaming(iPhoneで非フルスクリーン再生),
httpTakStreaming(flvセグメントによるFlash用のストリーミング)
を作成して、大人数ライブストリーミングを実施したい。
できたらjsdo.itとwonderflにプログラムを出版してみんなからスゲーっていわれたい。

# わかっていること。

ffmpegの出力は、はじめに設定した要件をみたさないと出力されてこない。
(audio & videoのストリームを出力設定しているのに、videoしかデータがこない場合は、コンバートがいつまでまっても開始されない。)
audioのみの動作できました。この場合でもデータの入力は問題なく動作していたみたいです。

# 目標とする動作

・transcoderを作成し、変換対象としては、どんなメッセージでも動作するようにしておく。(済み)
・中途でデータが追加された場合出力をそれにあわせて、再生成して、動作させてやる。(済み)
transcoderの方で出力データを調整するので、Red5DataQueueの動作は、flvバイトデータをいつでもうけいれて、データを出力するという形にする。

# パッケージの覚え書き

com.ttProject.red5 red5関連の動作
com.ttProject.flazr flazr関連の動作
com.ttProject.xuggle xuggle関連の動作
com.ttProject.streaming segment作成関連の動作
com.ttProject.output 出力調整の動作

# ライブラリの依存覚え書き

全体:slf4jとそのロガー関連
com.ttProject.red5:red5のライブラリ、apache.minaベース
com.ttProject.flazr:flazrのライブラリ、jbossベース
com.ttProject.xuggle:xuggle-xugglerのライブラリ
com.ttProject.streaming:特に必要なライブラリなし。
com.ttProject.output:ftpだけ、apache.netが必要
