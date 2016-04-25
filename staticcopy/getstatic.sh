#!/bin/bash
# prepare static content from localhost:8080

# *** dependencies ***
# brew install jq
# npm install
grunt

rm -rf content
mkdir content
cd content

curl http://localhost:8080/ > index.html

sides=( "corp" "runner" )

# create directories without sides
dirs=(
"static"
"static/css"
"static/js"
"static/js/vendor"
"static/img"
"static/img/blog"
"static/img/cards"
"static/fonts"
"JSON"
"Cards"
"DPStats"
"JSON/Cards"
"JSON/Identity"
"JSON/Tournament"
"MDSIdentity"
"Blog"
)
for dir in "${dirs[@]}"
do
    mkdir $dir
done

# create directories with sides
for side in "${sides[@]}"
do
    dirs=(
    "JSON/Cards/$side"
    "JSON/Tournament/$side"
    )
    for dir in "${dirs[@]}"
    do
        mkdir $dir
    done
done

# download static files
files=(
"JSON/Cardpool"
"JSON/Cardpacks"
"JSON/Cardpoolnames"
"JSON/Cardpacknames"
"JSON/FactionsOverTime"
"404"
"soon"
"Info"
)

for file in "${files[@]}"
do
    touch $file
    curl http://localhost:8080/$file > $file
done

mv soon soon.html
mv 404 404.html
mv Info Info.html
curl http://localhost:8080/Cards > Cards/index.html

# copy static folders
# skip favicons
cp -rf ../../src/main/resources/static/* static/
rm -rf static/favicons

# blog
curl http://localhost:8080/Blog > Blog/index.html
blogs=( $(curl http://localhost:8080/JSON/Blog/Teasers | jq -r '.[].url' ) )
for blog in "${blogs[@]}"
do
    mkdir Blog/$blog
    curl http://localhost:8080/Blog/$blog > "Blog/$blog/index.html"
done
cp ../../src/main/resources/static/img/blog/* static/img/blog/

# card images
cp ../../src/main/resources/static/img/cards/* static/img/cards/

packs=( $(curl http://localhost:8080/JSON/Cardpool | jq -r '.[].title' | sed 's/ /%20/g') "Last%203%20aggregated" )

# for each pack
for pack in "${packs[@]}"
do
    echo "Calculating card pack: $pack"
    for side in "${sides[@]}"
    do
        packfiles=(
        "JSON/Cards/$side/$pack"
        "JSON/Tournament/$side/$pack"
        )
        for packfile in "${packfiles[@]}"
        do
            curl http://localhost:8080/$packfile > "${packfile//%20/ }"
        done
    done

    mkdir "DPStats/${pack//%20/ }"
    curl http://localhost:8080/DPStats/$pack > "DPStats/${pack//%20/ }/index.html"

    mkdir "MDSIdentity/${pack//%20/ }"
    mkdir "JSON/Identity/${pack//%20/ }"

    # for each id
    idsrunner=( $(curl http://localhost:8080/JSON/Tournament/runner/$pack | jq -r '.ids[].title' | sed 's/ /%20/g') )
    idscorp=( $(curl http://localhost:8080/JSON/Tournament/corp/$pack | jq -r '.ids[].title' | sed 's/ /%20/g') )
    ids=( "${idsrunner[@]}" "${idscorp[@]}" )
    for identity in "${ids[@]}"
    do
        mkdir "MDSIdentity/${pack//%20/ }/${identity//%20/ }"
        echo "Calculating $identity for $pack"
        curl http://localhost:8080/MDSIdentity/$pack/$identity > "MDSIdentity/${pack//%20/ }/${identity//%20/ }/index.html"
        curl http://localhost:8080/JSON/Identity/$pack/$identity > "JSON/Identity/${pack//%20/ }/${identity//%20/ }"
    done
done

packs=( $(curl http://localhost:8080/JSON/Cardpacknames | jq -r '.[]' | sed 's/ /%20/g') )

for pack in "${packs[@]}"
do
    echo "Calculating card usage in pack: $pack"
    curl http://localhost:8080/JSON/Cards/corp/$pack > "JSON/Cards/corp/${pack//%20/ }"
    curl http://localhost:8080/JSON/Cards/runner/$pack > "JSON/Cards/runner/${pack//%20/ }"
done