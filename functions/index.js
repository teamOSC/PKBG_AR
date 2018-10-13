const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();


exports.checkAndUpdateHit = functions.database.ref('/games/{gameId}/hits/{hitsId}')
  .onCreate((snapshot, context) => {
    const hit = snapshot.val();
    console.log(hit)

    const players = admin.database().ref('/games').child(context.params.gameId).child('players')

    return players.once('value',  (snapshot) => {

      var hitTo;
      snapshot.forEach((childSnapshot) => {
        var childKey = childSnapshot.key;
        var childData = childSnapshot.val();
        console.log(childData)

        let health = childData.health || 100;

        if (childKey !== hit.hitBy) {
          hitTo = childKey

          if (hit.type.toString() === '0') {
            health = health - 100;
          }
          if (hit.type.toString() === '1') {
            health = health - 50;
          }
          console.log(health)
          snapshot.ref.parent.child('players').child(hitTo).child('health').set(health)
        }

      });

    });



  });
