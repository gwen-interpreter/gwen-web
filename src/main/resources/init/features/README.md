Features
========

Put your gherkin feature files and any associative meta files here.

What is associative meta?
-------------------------

Associtive meta is enabled by default through the `gwen.associative.meta=true` setting in the root `gwen.conf` file. Meta files that reside in the same location and have the same name as features, are loaded for those features only. 

For example, consider the folowing files in the features directory:
- fileA.feature
- fileA.meta
- fileB.feature
- fileB.meta
- fileC.feature

When Gwen executes fileA.feature, then only fileA.meta will be loaded and fileB.meta will not. Similarly for fileB.feature. However fileC.feature does not have an associated meta and therefore no meta file in this directory is loaded. Any shared meta in the `meta` folder however, would be loaded for all features.

Learn more:
- [Meta load strategies](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features#meta-strategies)
- [Associatve meta setting](https://github.com/gwen-interpreter/gwen/wiki/Runtime-Settings#gwenassociativemeta)
