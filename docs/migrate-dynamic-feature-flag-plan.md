# 概要
前提として当アプリは複数のSNSのAPIやそれらフォークをサポートしている。  
SNSによって使える機能やAPIやエンドポイントが異なるため、  
アプリではこれを識別し、ロジックの制御やUIの表示分けを行っていた。  
より多くのソフトウェアやフォークをサポートしていくために、この仕組みを改善する。

# 現状の問題点
現状はサーバーから取得できるnodeinfoや/meta, /instance情報を元に機能の判別をしている。  
これ自体は問題ないが、このnodeinfo, meta, instanceなどの構造に深く依存してしまっている。  
また依存している箇所ごとに複雑な評価処理を行なっていて可読性が低くなってしまっている。
```Kotlin
when (account.instanceType) {
                    Account.InstanceType.MISSKEY, Account.InstanceType.FIREFISH -> {
                        postUnReaction(deleteReaction.noteId)
                                && (noteCaptureAPIProvider.get(account)
                            ?.isCaptured(deleteReaction.noteId.noteId) == true
                                || (note.reactionCounts.any { it.me }
                                && noteDataSource.add(note.onIUnReacted())
                            .getOrThrow() != AddResult.Canceled))
                    }
                    Account.InstanceType.MASTODON, Account.InstanceType.PLEROMA -> {
                        val nodeInfo = nodeInfoRepository.find(account.getHost())
                            .getOrThrow()
                        if (nodeInfo.type !is NodeInfo.SoftwareType.Mastodon.Fedibird
                            && nodeInfo.type !is NodeInfo.SoftwareType.Mastodon.Kmyblue
                        ) {
                            if (!note.isSupportEmojiReaction) {
                                return@withContext false
                            }
                        }
                        val res = mastodonAPIProvider.get(account)
                            .deleteReaction(
                                deleteReaction.noteId.noteId,
                                Reaction(deleteReaction.reaction).getNameAndHost()
                            )
                            .throwIfHasError()
                            .body()
                        noteDataSourceAdder.addTootStatusDtoIntoDataSource(
                            account,
                            requireNotNull(res)
                        )
                        true
                    }
                }
```

# 解決策
解決策として、アプリ内部に動的な機能フラグを構築し、  
内部処理はこのフラグを元に使用できる機能や分岐を行うようにする。    
またこの機能はユーザーが設定画面などからフラグを操作できるようにし、  
アプリが想定していないForkなどであっても、フラグをON/OFFできるようにしフォールバックが提供されるようにする。  
今回は実施しないがこの機能フラグの状態をサーバーから取得できるようにし、  
ソースコードを直接改変することなくある程度のソフトウェアに対応できるようにする。  

# 大まかな設計
## 機能フラグオブジェクト
プリミティブな値とenumのみサポートする。  
実際に必要なフラグやenumは内部のnodeinfo, meta, instanceInfoを使った処理を洗い出す必要性がある。

### プロパティ
apiType, apiVersionは大まかなAPIのスキーマを選択する。  
内部的な処理としてはRetrofitのインターフェースの選択に使われることを想定。
- apiType(misskey, mastodon, pleroma)
- apiVersion
- reaction
  - isSupportReaction
  - maxReactionsPerAccount
  - unicodeFormatType
    - needColon
    - dontNeedColon
- supportMisskeyAntenna
- supportFireshRecommendTimeline
- supportGlobalTimeline
- supportLocalTimeline
- supportMisskeyGallery   

↑無数にあるので洗い出しを行なった上で合理性の検証を必ずしたい。  

## 機能フラグRepository
内部的にはNodeInfo, InstanceInfo, Metaなどの情報を用いて、  
機能フラグのインスタンスを生成することになる。  
確認したいこととして、機能フラグの構築に失敗してしまうと、  
全機能が使用できなくなってしまう可能性があるのと、  
構築が遅い場合は起動や一部処理が落ちる可能性がある。  
そのため、構築するときに使用されるデータが存在しない可能性や遅延が起きる可能性を考慮したい。  
インターフェースとしてはインスタンスのドメイン部を引数に受け取り、  
対応する機能フラグオブジェクトを返すようにする。  
またユーザーがインスタンスごとに機能フラグを設定できるようにするために、  
インスタンスのドメイン部ごとに機能フラグを永続化できるようにする必要性がある。  

## 機能フラグテンプレート
インスタンスやソフトウェアの種別ごとにデフォルトとして用意される機能フラグのデフォルト値集。  

# 機能フラグ設定画面
機能フラグをユーザーが手動で更新するための画面。

- 機能フラグを有効にする(実際は必須でONになっているがユーザーが変更できるようにするという意味でON)
  - 変更することで発生するリスクを明記
    - 対象インスタンス選択DROPDOWN
      - ログイン済みのアカウントのインスタンスのリスト + Intentで前の画面から値を貰えたあばいは追加表示
    - 全ての設定項目を列挙




```
nodeinfoとmetaをmodelクラスに変換した構造に内部実装が依存した状態をやめる

構造ベースの依存ではなく動的な機能フラグをベースとした基盤を作成する。

動的な機能フラグの構築は自体はnodeinfoとmetaなどの情報から構築するようにする。

また機能フラグは原則としてプリミティブとenumをサポートするようにする。

機能フラグはユーザーが認証画面や設定画面から編集できるようにし、
アプリが想定していないフォークであってもある程度の機能が動作するようにする。


このためにデフォルトの挙動を表す機能フラグテンプレートを用意しnodeinfoの情報を元に
このテンプレートの判別をする。

ユーザーの設定はオーバーライドダイナミックフィーチャーフラグとしてDBに保存する。


優先度は
ユーザーの設定>アプリのテンプレート

将来手にはこのテンプレートをサーバーから配信できるようにするが今はしない
```